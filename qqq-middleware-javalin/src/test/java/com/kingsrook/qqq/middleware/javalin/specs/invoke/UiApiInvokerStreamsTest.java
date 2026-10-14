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

package com.kingsrook.qqq.middleware.javalin.specs.invoke;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


/*******************************************************************************
 ** Streaming downloads and repeatable multipart request bodies.
 *******************************************************************************/
class UiApiInvokerStreamsTest
{
   private QApplicationJavalinServer server;



   /*******************************************************************************
    ** Restore server and registry state.
    *******************************************************************************/
   @AfterEach
   void cleanUp()
   {
      if(server != null)
      {
         server.stop();
      }
      UiApiInProcessSessions.clearForTests();
      QJavalinImplementation.setSessionCookieHttpOnly(false);
   }



   /*******************************************************************************
    ** Export a real table through the spec pipeline and consume its CSV stream.
    *******************************************************************************/
   @Test
   void testCsvStream() throws Exception
   {
      TestUtils.primeTestDatabase();
      server = new QApplicationJavalinServer(new UiApiInProcessSessionsTest.SessionApplication()).withPort(0)
         .withServeFrontendNext(false).withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false);
      server.start();
      UiApiInvoker invoker = UiApiInvoker.forServer(server);
      String token = UiApiInProcessSessions.register(new QSession(), Duration.ofMinutes(1));
      UiApiRequest request = new UiApiRequest().withMethod(HttpMethod.POST).withPath("/table/{tableName}/export")
         .withPathParam("tableName", "person").withJsonBody(Map.of("format", "csv"))
         .withCredentials(UiApiCredentials.ofInProcessSession(token));
      try(UiApiStreamResponse response = invoker.invokeForStream(request))
      {
         assertEquals(200, response.getStatus());
         assertTrue(response.isSuccess());
         assertThat(response.getHeader("CONTENT-TYPE")).startsWith("text/csv");
         assertThat(response.getHeaders()).isNotEmpty();
         assertNull(response.getHeader("Missing"));
         assertThat(new String(response.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).contains("Darin", "Kelkhoff");
         assertEquals(0, depth().get());
         try(UiApiStreamResponse another = invoker.invokeForStream(request))
         {
            assertEquals(200, another.getStatus());
         }
      }
      UiApiInProcessSessions.revoke(token);
      try(UiApiStreamResponse response = invoker.invokeForStream(request))
      {
         assertEquals(401, response.getStatus());
         assertFalse(response.isSuccess());
         assertThat(response.readBodyAsString()).contains("Invalid in-process session");
      }
   }



   /*******************************************************************************
    ** Response closure delegates to the stream, and header names ignore case.
    *******************************************************************************/
   @Test
   void testResponseClose() throws Exception
   {
      InputStream stream = mock(InputStream.class);
      try(UiApiStreamResponse response = new UiApiStreamResponse(299, Map.of("X-Test", List.of("first", "second")), stream))
      {
         assertTrue(response.isSuccess());
         assertEquals("first", response.getHeader("x-test"));
      }
      verify(stream).close();
      assertFalse(new UiApiStreamResponse(300, Map.of(), InputStream.nullInputStream()).isSuccess());
   }



   /*******************************************************************************
    ** Streaming and buffered calls share validation and restore the depth on errors.
    *******************************************************************************/
   @Test
   void testSharedValidation() throws Exception
   {
      UiApiInvoker invoker = invoker();
      assertThrows(QException.class, () -> invoker.invokeForStream(new UiApiRequest()));
      assertThrows(QException.class, () -> invoker.invokeForStream(new UiApiRequest().withMethod(HttpMethod.GET).withPath("/raw")));
      UiApiRequest secured = new UiApiRequest().withMethod(HttpMethod.GET).withPath("/metaData");
      assertThrows(QException.class, () -> invoker.invokeForStream(secured));
      assertThrows(QException.class, () -> invoker.invoke(secured));
      secured.withSessionUUID("test");
      depth().set(UiApiInvoker.MAX_DEPTH);
      try
      {
         assertThrows(QException.class, () -> invoker.invokeForStream(secured));
         assertThrows(QException.class, () -> invoker.invoke(secured));
         assertEquals(UiApiInvoker.MAX_DEPTH, depth().get());
      }
      finally
      {
         depth().remove();
      }
      UiApiRequest invalidBody = multipartRequest().withJsonBody(Map.of());
      assertThrows(QException.class, () -> invoker.invokeForStream(invalidBody));
      assertThrows(QException.class, () -> invoker.invoke(invalidBody));
      assertEquals(0, depth().get());
   }



   /*******************************************************************************
    ** Parse multipart framing, text and binary parts; streams open lazily per send.
    *******************************************************************************/
   @Test
   void testMultipartBody() throws Exception
   {
      AtomicInteger opens = new AtomicInteger();
      AtomicInteger closes = new AtomicInteger();
      UiApiRequest request = multipartRequest().withFormParam("caption", "hello & café")
         .withFormParam("empty", null)
         .withMultipartFile("second", "unsafe\r\n\"name.bin", "application/octet-stream", () ->
         {
            opens.incrementAndGet();
            return (new ByteArrayInputStream(new byte[] { 0, 1, 2, (byte) 255 })
            {
               /*******************************************************************************
                ** Observe that the publisher closes each supplied stream.
                *******************************************************************************/
               @Override
               public void close()
               {
                  closes.incrementAndGet();
               }
            });
         });
      UiApiInvoker invoker = invoker();
      UiApiOperation operation = invoker.findOperation(request.getMethod(), request.getPath()).orElseThrow();
      HttpRequest http = invoker.buildHttpRequest(operation, request);
      String boundary = http.headers().firstValue("Content-Type").orElseThrow().split("boundary=")[1];
      assertEquals(0, opens.get());
      assertEquals(-1, http.bodyPublisher().orElseThrow().contentLength());
      byte[] first = body(http);
      assertThat(body(http)).isEqualTo(first);
      assertEquals(2, opens.get());
      assertEquals(2, closes.get());
      String wire = new String(first, StandardCharsets.ISO_8859_1);
      String[] parts = wire.split("--" + boundary, -1);
      assertEquals(6, parts.length);
      assertThat(parts[1]).startsWith("\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n");
      assertThat(new String(first, StandardCharsets.UTF_8)).contains("hello & café");
      assertThat(parts[2]).isEqualTo("\r\nContent-Disposition: form-data; name=\"empty\"\r\n\r\n\r\n");
      assertThat(parts[3]).contains("name=\"upload\"; filename=\"people.csv\"", "Content-Type: text/csv\r\n\r\nfirstName\r\nAda\r\n");
      assertThat(parts[4]).contains("filename=\"unsafename.bin\"", "\r\n\r\n" + new String(new byte[] { 0, 1, 2, (byte) 255 }, StandardCharsets.ISO_8859_1) + "\r\n");
      assertEquals("--\r\n", parts[5]);
      assertThat(invoker.buildHttpRequest(operation, request).headers().firstValue("Content-Type")).isNotEqualTo(http.headers().firstValue("Content-Type"));
   }



   /*******************************************************************************
    ** Multipart excludes JSON and prevents header injection through MIME types.
    *******************************************************************************/
   @Test
   void testMultipartValidation() throws Exception
   {
      UiApiInvoker invoker = invoker();
      UiApiRequest request = multipartRequest();
      UiApiOperation operation = invoker.findOperation(request.getMethod(), request.getPath()).orElseThrow();
      assertThrows(QException.class, () -> invoker.buildHttpRequest(operation, request.withJsonBody(Map.of())));
      request.withJsonBody(null).withMultipartFile("file", "name", "text/plain\r\nInjected: yes", InputStream::nullInputStream);
      assertThrows(QException.class, () -> invoker.buildHttpRequest(operation, request));
   }



   /*******************************************************************************
    ** Files may only go to operations whose spec declares a multipart body.
    *******************************************************************************/
   @Test
   void testMultipartOnlyForMultipartOperations() throws Exception
   {
      UiApiInvoker invoker = invoker();
      UiApiRequest init = multipartRequest();
      assertTrue(UiApiInvoker.declaresMultipartBody(invoker.findOperation(init.getMethod(), init.getPath()).orElseThrow()));
      UiApiRequest query = new UiApiRequest().withMethod(HttpMethod.POST).withPath("/table/{tableName}/query")
         .withPathParam("tableName", "person").withSessionUUID("test")
         .withMultipartFile("upload", "people.csv", "text/csv", InputStream::nullInputStream);
      assertFalse(UiApiInvoker.declaresMultipartBody(invoker.findOperation(query.getMethod(), query.getPath()).orElseThrow()));
      QException exception = assertThrows(QException.class, () -> invoker.invoke(query));
      assertTrue(exception.getMessage().contains("multipart/form-data"));
   }



   /*******************************************************************************
    ** Form fields go as multipart to multipart-only operations (process step),
    ** and stay URL-encoded otherwise.
    *******************************************************************************/
   @Test
   void testFormFieldsFollowDeclaredBody() throws Exception
   {
      UiApiInvoker invoker = invoker();
      UiApiRequest step = new UiApiRequest().withMethod(HttpMethod.POST).withPath("/processes/{processName}/{processUUID}/step/{stepName}")
         .withPathParam("processName", "person.bulkInsert").withPathParam("processUUID", "u").withPathParam("stepName", "receiveFileMapping")
         .withSessionUUID("test").withFormParam("values", "{\"savedBulkLoadProfileId\":1}");
      HttpRequest stepHttp = invoker.buildHttpRequest(invoker.findOperation(step.getMethod(), step.getPath()).orElseThrow(), step);
      assertThat(stepHttp.headers().firstValue("Content-Type").orElseThrow()).startsWith("multipart/form-data; boundary=");

      UiApiRequest logout = new UiApiRequest().withMethod(HttpMethod.POST).withPath("/logout").withSessionUUID("test").withFormParam("a", "b");
      UiApiOperation logoutOperation = invoker.findOperation(logout.getMethod(), logout.getPath()).orElse(null);
      if(logoutOperation != null && !UiApiInvoker.declaresMultipartBody(logoutOperation))
      {
         HttpRequest logoutHttp = invoker.buildHttpRequest(logoutOperation, logout);
         assertEquals("application/x-www-form-urlencoded", logoutHttp.headers().firstValue("Content-Type").orElseThrow());
      }
   }



   /*******************************************************************************
    ** In-process credentials take precedence and no other credential is forwarded.
    *******************************************************************************/
   @Test
   void testCredentialHeaders() throws Exception
   {
      UiApiInvoker invoker = invoker();
      UiApiRequest request = new UiApiRequest().withMethod(HttpMethod.GET).withPath("/metaData")
         .withCredentials(new UiApiCredentials("cookie", "id", "bearer", "key", "in-process"));
      HttpRequest http = invoker.buildHttpRequest(invoker.findOperation(request.getMethod(), request.getPath()).orElseThrow(), request);
      assertEquals("in-process", http.headers().firstValue(UiApiInProcessSessions.HEADER).orElseThrow());
      for(String header : List.of("Cookie", "Authorization", "x-api-key"))
      {
         assertTrue(http.headers().firstValue(header).isEmpty());
      }
   }



   /*******************************************************************************
    ** A process-init multipart request, with its file supplier reusable per attempt.
    *******************************************************************************/
   private static UiApiRequest multipartRequest()
   {
      return (new UiApiRequest().withMethod(HttpMethod.POST).withPath("/processes/{processName}/init")
         .withPathParam("processName", "person.bulkInsert").withSessionUUID("test")
         .withMultipartFile("upload", "people.csv", "text/csv", () -> new ByteArrayInputStream("firstName\r\nAda".getBytes(StandardCharsets.UTF_8))));
   }



   /*******************************************************************************
    ** An invoker used only for validation and request building, without network I/O.
    *******************************************************************************/
   private static UiApiInvoker invoker() throws QException
   {
      MiddlewareVersionV1 version = new MiddlewareVersionV1();
      version.setQInstance(TestUtils.defineInstance());
      return (UiApiInvoker.forBaseUrl("http://127.0.0.1:1", version));
   }



   /*******************************************************************************
    ** Inspect invocation depth without adding production test hooks.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static ThreadLocal<Integer> depth() throws Exception
   {
      Field field = UiApiInvoker.class.getDeclaredField("DEPTH");
      field.setAccessible(true);
      return ((ThreadLocal<Integer>) field.get(null));
   }



   /*******************************************************************************
    ** Consume the small fixture using the same reactive publisher contract as HTTP.
    *******************************************************************************/
   private static byte[] body(HttpRequest request) throws Exception
   {
      HttpResponse.BodySubscriber<byte[]> subscriber = HttpResponse.BodySubscribers.ofByteArray();
      request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>()
      {
         /*******************************************************************************
          ** Forward subscription and demand.
          *******************************************************************************/
         @Override
         public void onSubscribe(Flow.Subscription subscription)
         {
            subscriber.onSubscribe(subscription);
         }


         /*******************************************************************************
          ** Adapt each publisher buffer to the response subscriber's buffer list.
          *******************************************************************************/
         @Override
         public void onNext(ByteBuffer item)
         {
            subscriber.onNext(List.of(item));
         }


         /*******************************************************************************
          ** Forward publisher failures to the test future.
          *******************************************************************************/
         @Override
         public void onError(Throwable throwable)
         {
            subscriber.onError(throwable);
         }


         /*******************************************************************************
          ** Complete the test body.
          *******************************************************************************/
         @Override
         public void onComplete()
         {
            subscriber.onComplete();
         }
      });
      return (subscriber.getBody().toCompletableFuture().get(5, TimeUnit.SECONDS));
   }
}
