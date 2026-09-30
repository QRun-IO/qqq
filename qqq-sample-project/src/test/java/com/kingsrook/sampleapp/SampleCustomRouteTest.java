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


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.javalin.metadata.JavalinRouteProviderMetaData;
import com.kingsrook.qqq.middleware.javalin.routeproviders.ProcessBasedRouterPayload;
import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.RouteAuthenticatorInterface;
import com.kingsrook.qqq.middleware.javalin.routeproviders.contexthandlers.DefaultRouteProviderContextHandler;
import com.kingsrook.sampleapp.authentication.LocalDemoRouteAuthenticator;
import com.kingsrook.sampleapp.metadata.DynamicSiteProcessMetaDataProducer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native HTTP contracts using the sample's routes and explicit fixture variants.
 *******************************************************************************/
class SampleCustomRouteTest
{
   private static final String AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString("sample:sample-only".getBytes(StandardCharsets.UTF_8));
   private static final byte[] BINARY_BODY = {0, 1, 127, -128, -1};
   private static final AtomicInteger PROCESS_CALLS = new AtomicInteger();

   private final BlockingQueue<List<Boolean>> completedRequests = new LinkedBlockingQueue<>();
   private final AtomicReference<Javalin> service = new AtomicReference<>();
   private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   private SampleJavalinServer server;

   @TempDir
   Path storageDirectory;



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanUp()
   {
      try
      {
         if(server != null)
         {
            server.stop();
         }
      }
      finally
      {
         client.close();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Each supported verb crosses the actual HTTP-to-process boundary.
    *******************************************************************************/
   @Test
   void testMethodsAndDecodedRequestData() throws Exception
   {
      QInstance instance = fixtureInstance();
      route(instance, "/dynamic-site/<pagePath>").setMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
      start(instance);
      for(String method : List.of("GET", "POST", "PUT", "PATCH", "DELETE"))
      {
         String body = "POST".equals(method) ? "first=one&first=two&escaped=a%2Bb" : "GET".equals(method) ? "" : "raw payload: café";
         HttpResponse<String> response = send(request("/dynamic-site/detail%20page?tag=first&tag=second&value=a%2Bb", AUTHORIZATION)
            .header("Content-Type", "POST".equals(method) ? "application/x-www-form-urlencoded" : "text/plain; charset=utf-8")
            .header("Cookie", "sample-cookie=owned-value")
            .header("X-Sample-Request", "request-marker")
            .method(method, HttpRequest.BodyPublishers.ofString(body)));
         assertEquals(202, response.statusCode(), response.body());
         assertEquals("sample", response.headers().firstValue("X-Route-Contract").orElseThrow());
         assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("application/json"));
         JSONObject result = JsonUtils.toJSONObject(response.body());
         assertEquals(method, result.getString("method"));
         assertEquals(String.class.getName(), result.getString("methodType"));
         assertEquals("/dynamic-site/detail%20page", result.getString("path"));
         assertEquals("detail page", result.getJSONObject("pathParams").getString("pagePath"));
         assertEquals(List.of("first", "second"), result.getJSONObject("queryParams").getJSONArray("tag").toList());
         assertEquals(List.of("a+b"), result.getJSONObject("queryParams").getJSONArray("value").toList());
         assertEquals("owned-value", result.getString("cookie"));
         assertEquals("request-marker", result.getString("requestHeader"));
         assertEquals(body, result.getString("body"));
         if("POST".equals(method))
         {
            assertEquals(List.of("one", "two"), result.getJSONObject("formParams").getJSONArray("first").toList());
            assertEquals(List.of("a+b"), result.getJSONObject("formParams").getJSONArray("escaped").toList());
         }
         else if("GET".equals(method))
         {
            assertTrue(result.getJSONObject("formParams").isEmpty());
         }
      }
      assertEquals(5, PROCESS_CALLS.get());
   }



   /*******************************************************************************
    ** Rejected authentication never reaches a private file or process.
    *******************************************************************************/
   @Test
   void testPublicAndAuthenticatedRoutes() throws Exception
   {
      start(fixtureInstance());
      HttpResponse<String> publicFile = send(request("/public/index.html", ""));
      assertEquals(200, publicFile.statusCode());
      assertTrue(publicFile.body().contains("Welcome to the public site"));
      for(String path : List.of("/private", "/private/", "/private/index.html", "/private/app.js", "/dynamic-site/details"))
      {
         for(String authorization : List.of("", "Basic !!!", "Basic " + Base64.getEncoder().encodeToString("sample:wrong".getBytes(StandardCharsets.UTF_8))))
         {
            HttpResponse<String> denied = send(request(path, authorization));
            assertEquals(401, denied.statusCode(), path);
            assertEquals("Basic realm=\"QQQ local demonstration\"", denied.headers().firstValue("WWW-Authenticate").orElseThrow());
            assertFalse(denied.body().contains("Welcome to the private site"));
            assertFalse(denied.body().contains("current-route"));
            assertFalse(denied.headers().firstValue("X-Route-Contract").isPresent());
            assertEquals(0, PROCESS_CALLS.get());
         }
      }
      for(String path : List.of("/private", "/private/", "/private/index.html"))
      {
         HttpResponse<String> allowed = send(request(path, AUTHORIZATION));
         assertEquals(200, allowed.statusCode(), path);
         assertTrue(allowed.body().contains("Welcome to the private site"));
      }
      HttpResponse<String> asset = send(request("/private/app.js", AUTHORIZATION));
      assertEquals(200, asset.statusCode());
      assertTrue(asset.body().contains("current-route"));
      assertEquals(202, send(request("/dynamic-site/details", AUTHORIZATION)).statusCode());
      assertEquals(1, PROCESS_CALLS.get());
   }



   /*******************************************************************************
    ** Encoded and literal traversal cannot expose sibling files or SQL resources.
    *******************************************************************************/
   @Test
   void testTraversalAndUnknownRoutes() throws Exception
   {
      start(fixtureInstance());
      for(String path : List.of("/public/../private/index.html", "/public/%2e%2e/private/index.html", "/public/%2e%2e%2fprivate/index.html",
         "/public/../../prime-test-database.sql", "/public/%2e%2e/%2e%2e/prime-test-database.sql", "/public/missing.js", "/unknown-route", "/private-sibling/index.html"))
      {
         HttpResponse<String> response = send(request(path, ""));
         assertTrue(response.statusCode() >= 400 && response.statusCode() < 500, path + ": " + response.statusCode());
         assertFalse(response.body().contains("Welcome to the private site"), path);
         assertFalse(response.body().contains("CREATE TABLE"), path);
      }
      assertEquals(0, PROCESS_CALLS.get());
   }



   /*******************************************************************************
    ** Failed processes terminate cleanly and leave the next request usable.
    *******************************************************************************/
   @Test
   void testProcessFailureAndContextCleanup() throws Exception
   {
      start(fixtureInstance());
      HttpResponse<String> denied = send(request("/dynamic-site/details", ""));
      assertEquals(401, denied.statusCode());
      assertContextCleared();
      assertEquals(0, PROCESS_CALLS.get());
      HttpResponse<String> failed = send(request("/dynamic-site/failure", AUTHORIZATION));
      assertEquals(500, failed.statusCode(), failed.body());
      assertTrue(JsonUtils.toJSONObject(failed.body()).has("error"));
      assertFalse(failed.headers().firstValue("X-Route-Contract").isPresent());
      assertContextCleared();
      assertEquals(202, send(request("/dynamic-site/details", AUTHORIZATION)).statusCode());
      assertContextCleared();
      assertEquals(2, PROCESS_CALLS.get());
   }



   /*******************************************************************************
    ** Exceptions from either file or process authentication do not leak context.
    *******************************************************************************/
   @Test
   void testAuthenticatorFailureAndContextCleanup() throws Exception
   {
      QInstance instance = fixtureInstance();
      route(instance, "/private").setRouteAuthenticator(new QCodeReference(FailingAuthenticator.class));
      route(instance, "/dynamic-site/<pagePath>").setRouteAuthenticator(new QCodeReference(FailingAuthenticator.class));
      start(instance);
      for(String path : List.of("/private", "/private/app.js", "/dynamic-site/details"))
      {
         HttpResponse<String> response = send(request(path, AUTHORIZATION).header("X-Sample-Auth-Failure", "true"));
         assertEquals(401, response.statusCode(), response.body());
         assertTrue(JsonUtils.toJSONObject(response.body()).getString("error").contains("Synthetic authentication failure"));
         assertFalse(response.body().contains("Welcome to the private site"));
         assertEquals(0, PROCESS_CALLS.get());
         assertContextCleared();
      }
      assertEquals(202, send(request("/dynamic-site/details", AUTHORIZATION)).statusCode());
      assertContextCleared();
      assertEquals(1, PROCESS_CALLS.get());
   }



   /*******************************************************************************
    ** Binary and redirect outputs survive the process payload boundary.
    *******************************************************************************/
   @Test
   void testBinaryAndRedirectResponses() throws Exception
   {
      start(fixtureInstance());
      HttpResponse<byte[]> binary = client.send(request("/dynamic-site/binary", AUTHORIZATION).build(), HttpResponse.BodyHandlers.ofByteArray());
      assertEquals(200, binary.statusCode());
      assertEquals("application/octet-stream", binary.headers().firstValue("Content-Type").orElseThrow().split(";")[0]);
      assertArrayEquals(BINARY_BODY, binary.body());
      HttpResponse<String> redirect = send(request("/dynamic-site/redirect", AUTHORIZATION));
      assertEquals(307, redirect.statusCode());
      assertEquals("/public/index.html", redirect.headers().firstValue("Location").orElseThrow());
      assertEquals(2, PROCESS_CALLS.get());
   }



   /*******************************************************************************
    ** Stream exact bytes from the sample filesystem backend and refuse missing data.
    *******************************************************************************/
   @Test
   void testStorageBackedResponse() throws Exception
   {
      QInstance instance = fixtureInstance();
      ((FilesystemBackendMetaData) instance.getBackend(SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME)).setBasePath(storageDirectory.toString());
      Path storedFile = Files.createDirectories(storageDirectory.resolve("cities")).resolve("response.bin");
      Files.write(storedFile, BINARY_BODY);
      start(instance);
      HttpResponse<byte[]> response = client.send(request("/dynamic-site/storage", AUTHORIZATION).build(), HttpResponse.BodyHandlers.ofByteArray());
      assertEquals(200, response.statusCode());
      assertEquals("application/octet-stream", response.headers().firstValue("Content-Type").orElseThrow().split(";")[0]);
      assertArrayEquals(Files.readAllBytes(storedFile), response.body());
      assertContextCleared();
      Files.delete(storedFile);
      HttpResponse<String> missing = send(request("/dynamic-site/storage", AUTHORIZATION));
      assertEquals(500, missing.statusCode(), missing.body());
      assertTrue(JsonUtils.toJSONObject(missing.body()).has("error"));
      assertContextCleared();
   }



   /*******************************************************************************
    ** Metadata selects both directions of the custom context handler.
    *******************************************************************************/
   @Test
   void testCustomContextHandler() throws Exception
   {
      QInstance instance = fixtureInstance();
      route(instance, "/dynamic-site/<pagePath>").setContextHandler(new QCodeReference(CustomContextHandler.class));
      start(instance);
      HttpResponse<String> response = send(request("/dynamic-site/details", AUTHORIZATION));
      assertEquals(202, response.statusCode());
      assertEquals("mapped", JsonUtils.toJSONObject(response.body()).getString("custom"));
      assertEquals("mapped", response.headers().firstValue("X-Custom-Response").orElseThrow());
   }



   /*******************************************************************************
    ** Retain canonical metadata, substituting only the exercised extension points.
    *******************************************************************************/
   private QInstance fixtureInstance() throws QException
   {
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      route(instance, "/private").setRouteAuthenticator(new QCodeReference(LocalDemoRouteAuthenticator.class));
      route(instance, "/dynamic-site/<pagePath>").setRouteAuthenticator(new QCodeReference(LocalDemoRouteAuthenticator.class));
      instance.getProcess(DynamicSiteProcessMetaDataProducer.NAME).getBackendStep("DynamicSiteProcessStep").setCode(new QCodeReference(RouteContractStep.class));
      PROCESS_CALLS.set(0);
      return instance;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JavalinRouteProviderMetaData route(QInstance instance, String path)
   {
      return QJavalinMetaData.of(instance).getRouteProviders().stream().filter(route -> path.equals(route.getHostedPath())).findFirst().orElseThrow();
   }



   /*******************************************************************************
    ** Disable the optional dashboard so missing file paths have no UI fallback.
    *******************************************************************************/
   private void start(QInstance instance) throws Exception
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
      server.withJavalinConfigCustomizer(config -> config.requestLogger.http((context, milliseconds) ->
      {
         try
         {
            completedRequests.add(List.of(QContext.getQInstance() == null, QContext.getQSession() == null,
               QContext.getQBackendTransaction() == null, QContext.getActionStack() == null));
         }
         finally
         {
            QContext.clear();
         }
      }));
      server.start();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpRequest.Builder request(String path, String authorization)
   {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + path)).timeout(Duration.ofSeconds(5));
      if(!authorization.isEmpty())
      {
         builder.header("Authorization", authorization);
      }
      return builder;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> send(HttpRequest.Builder request) throws Exception
   {
      return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    ** Observe the request thread before the fixture's own cleanup.
    *******************************************************************************/
   private void assertContextCleared() throws InterruptedException
   {
      assertEquals(List.of(true, true, true, true), completedRequests.poll(5, TimeUnit.SECONDS));
   }



   /*******************************************************************************
    ** Explicit response variants of the canonical sample dynamic process.
    *******************************************************************************/
   public static class RouteContractStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         PROCESS_CALLS.incrementAndGet();
         ProcessBasedRouterPayload payload = input.getProcessPayload(ProcessBasedRouterPayload.class);
         String page = payload.getPathParams().get("pagePath");
         switch(page)
         {
            case "failure" -> throw new QException("Synthetic route failure");
            case "binary" ->
            {
               payload.setResponseBytes(BINARY_BODY);
               payload.setResponseHeaders(Map.of("Content-Type", "application/octet-stream"));
            }
            case "storage" ->
            {
               output.addValue("responseStorageInput", new StorageInput(SampleMetaDataProvider.TABLE_NAME_CITY).withReference("response.bin"));
               payload.setResponseHeaders(Map.of("Content-Type", "application/octet-stream"));
            }
            case "redirect" ->
            {
               payload.setRedirectURL("/public/index.html");
               payload.setStatusCode(307);
            }
            default ->
            {
               Map<String, Object> response = new LinkedHashMap<>();
               response.put("path", payload.getPath());
               response.put("method", payload.getMethod());
               response.put("methodType", input.getValue("method").getClass().getName());
               response.put("pathParams", payload.getPathParams());
               response.put("queryParams", payload.getQueryParams());
               response.put("formParams", payload.getFormParams());
               response.put("cookie", payload.getCookies().get("sample-cookie"));
               response.put("body", payload.getBodyString());
               Map<?, ?> headers = (Map<?, ?>) input.getValue("requestHeaders");
               response.put("requestHeader", headers.entrySet().stream().filter(entry -> "X-Sample-Request".equalsIgnoreCase(entry.getKey().toString())).map(Map.Entry::getValue).findFirst().orElse(null));
               response.put("custom", input.getValueString("custom"));
               payload.setResponseString(new JSONObject(response).toString());
               payload.setStatusCode(202);
               payload.setResponseHeaders(Map.of("Content-Type", "application/json", "X-Route-Contract", "sample"));
            }
         }
         output.setProcessPayload(payload);
      }
   }



   /*******************************************************************************
    ** Authenticator failures use the same safe native error path as denied access.
    *******************************************************************************/
   public static class FailingAuthenticator implements RouteAuthenticatorInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean authenticateRequest(Context context) throws QException
      {
         if("true".equals(context.header("X-Sample-Auth-Failure")))
         {
            throw new QAuthenticationException("Synthetic authentication failure");
         }
         return new LocalDemoRouteAuthenticator().authenticateRequest(context);
      }
   }



   /*******************************************************************************
    ** The extension may augment the default mappings without replacing them.
    *******************************************************************************/
   public static class CustomContextHandler extends DefaultRouteProviderContextHandler
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void handleRequest(Context context, RunProcessInput input)
      {
         super.handleRequest(context, input);
         input.addValue("custom", "mapped");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean handleResponse(Context context, RunProcessOutput output) throws QException
      {
         context.header("X-Custom-Response", output.getValueString("custom"));
         return super.handleResponse(context, output);
      }
   }
}
