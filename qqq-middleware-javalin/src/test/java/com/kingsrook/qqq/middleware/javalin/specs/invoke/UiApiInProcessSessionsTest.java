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


import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.executors.ExecutorSessionUtils;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.TestExtensionSpecs;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** JVM-local session registration, authentication and per-request isolation.
 *******************************************************************************/
class UiApiInProcessSessionsTest
{
   private QApplicationJavalinServer server;



   /*******************************************************************************
    ** Restore global test state.
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
      QContext.clear();
   }



   /*******************************************************************************
    ** Registration uses random URL-safe tokens and unknown tokens are harmless.
    *******************************************************************************/
   @Test
   void testRegisterResolveRevoke()
   {
      QSession original = session();
      String token = UiApiInProcessSessions.register(original, Duration.ofHours(24));
      String other = UiApiInProcessSessions.register(original, Duration.ofMinutes(1));
      assertThat(token).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(other);
      assertEquals(2, UiApiInProcessSessions.size());
      QSession resolved = UiApiInProcessSessions.resolve(token).orElseThrow();
      assertNotSame(original, resolved);
      assertEquals(original.getUuid(), resolved.getUuid());
      assertEquals("ssh-user", resolved.getUser().getIdReference());
      assertEquals(original.getPermissions(), resolved.getPermissions());
      UiApiInProcessSessions.revoke(token);
      UiApiInProcessSessions.revoke(token);
      UiApiInProcessSessions.revoke(null);
      assertTrue(UiApiInProcessSessions.resolve(token).isEmpty());
      assertTrue(UiApiInProcessSessions.resolve("unknown").isEmpty());
      assertTrue(UiApiInProcessSessions.resolve(null).isEmpty());
      assertEquals(1, UiApiInProcessSessions.size());
   }



   /*******************************************************************************
    ** Invalid lifetimes are rejected and expired entries are removed on both paths.
    *******************************************************************************/
   @Test
   void testExpiryAndTtl() throws Exception
   {
      for(Duration ttl : List.of(Duration.ZERO, Duration.ofSeconds(-1), Duration.ofHours(24).plusNanos(1)))
      {
         assertThrows(IllegalArgumentException.class, () -> UiApiInProcessSessions.register(session(), ttl));
      }
      assertThrows(IllegalArgumentException.class, () -> UiApiInProcessSessions.register(session(), null));
      String expired = UiApiInProcessSessions.register(session(), Duration.ofMillis(1));
      Thread.sleep(20);
      assertTrue(UiApiInProcessSessions.resolve(expired).isEmpty());
      assertEquals(0, UiApiInProcessSessions.size());
      UiApiInProcessSessions.register(session(), Duration.ofMillis(1));
      Thread.sleep(20);
      UiApiInProcessSessions.register(session(), Duration.ofMinutes(1));
      assertEquals(1, UiApiInProcessSessions.size());
   }



   /*******************************************************************************
    ** Both stored and returned snapshots isolate all mutable session collections.
    *******************************************************************************/
   @Test
   void testSnapshots()
   {
      QSession original = session().withValue("key", "original")
         .withSecurityKeyValues(Map.of("tenant", List.of(7)))
         .withBackendVariants(Map.of("tenant", 7))
         .withValueForFrontend("label", "original");
      original.setIdReference("id-reference");
      String token = UiApiInProcessSessions.register(original, Duration.ofMinutes(1));
      original.getUser().setFullName("changed");
      original.setValue("key", "changed");
      QSession copy = UiApiInProcessSessions.resolve(token).orElseThrow();
      assertEquals("SSH User", copy.getUser().getFullName());
      assertEquals("original", copy.getValue("key"));
      assertEquals("id-reference", copy.getIdReference());
      copy.getPermissions().clear();
      copy.getSecurityKeyValues().get("tenant").clear();
      copy.getBackendVariants().clear();
      copy.getValuesForFrontend().clear();
      copy.setValue("key", "request");
      QSession again = UiApiInProcessSessions.resolve(token).orElseThrow();
      assertThat(again.getPermissions()).containsExactly("person.READ");
      assertThat(again.getSecurityKeyValues().get("tenant")).containsExactly(7);
      assertThat(again.getBackendVariants()).containsEntry("tenant", 7);
      assertThat(again.getValuesForFrontend()).containsEntry("label", "original");
      assertEquals("original", again.getValue("key"));
   }



   /*******************************************************************************
    ** Only the actual loopback socket peer may present an in-process token.
    *******************************************************************************/
   @Test
   void testLoopbackAcceptance() throws Exception
   {
      String token = UiApiInProcessSessions.register(session(), Duration.ofMinutes(1));
      for(String address : List.of("127.0.0.1", "::1"))
      {
         assertEquals("ssh-user", ExecutorSessionUtils.resolveInProcessSession(address, token).getUser().getIdReference());
      }
      for(String address : List.of("10.1.2.3", "", "invalid address"))
      {
         assertEquals("Invalid in-process session", assertThrows(QAuthenticationException.class,
            () -> ExecutorSessionUtils.resolveInProcessSession(address, token)).getMessage());
      }
      assertThrows(QAuthenticationException.class, () -> ExecutorSessionUtils.resolveInProcessSession(null, token));
      assertThrows(QAuthenticationException.class, () -> ExecutorSessionUtils.resolveInProcessSession("127.0.0.1", "unknown"));
   }



   /*******************************************************************************
    ** Untrusted context headers must never be captured as in-process credentials.
    *******************************************************************************/
   @Test
   void testCredentialsFromContext()
   {
      Context context = mock(Context.class);
      when(context.header(UiApiInProcessSessions.HEADER)).thenReturn("untrusted");
      UiApiCredentials credentials = UiApiCredentials.fromContext(context);
      assertFalse(credentials.isPresent());
      assertEquals(null, credentials.inProcessSessionToken());
      verify(context, never()).header(UiApiInProcessSessions.HEADER);
      assertTrue(UiApiCredentials.ofInProcessSession("private-token").isPresent());
      assertThat(UiApiCredentials.ofInProcessSession("private-token").toString()).doesNotContain("private-token");
   }



   /*******************************************************************************
    ** Legacy authentication uses the same check and retains timezone and variants.
    *******************************************************************************/
   @Test
   void testLegacySetup() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      QJavalinImplementation.setQInstance(instance);
      String token = UiApiInProcessSessions.register(session(), Duration.ofMinutes(1));
      Context context = context(token, "api/legacy", "America/Chicago");
      when(context.queryParam("tableVariant")).thenReturn("{\"type\":\"tenant\",\"id\":7}");
      QSession resolved = QJavalinImplementation.setupSession(context, null);
      assertEquals("ssh-user", resolved.getUser().getIdReference());
      assertEquals("America/Chicago", resolved.getValue(QSession.VALUE_KEY_USER_TIMEZONE));
      assertThat(resolved.getBackendVariants()).containsEntry("tenant", 7);
      when(context.header(UiApiInProcessSessions.HEADER)).thenReturn("unknown");
      when(context.cookie("sessionUUID")).thenReturn("valid-mock-session");
      assertThrows(QAuthenticationException.class, () -> QJavalinImplementation.setupSession(context, null));
      when(context.header(UiApiInProcessSessions.HEADER)).thenReturn(token);
      when(context.req().getRemoteAddr()).thenReturn("10.1.2.3");
      assertThrows(QAuthenticationException.class, () -> QJavalinImplementation.setupSession(context, null));

      ////////////////////////////////////////////////////////////////////////////
      // a valid token from outside the JVM's loopback is refused, even when   //
      // forwarding headers claim loopback: only the socket peer is trusted    //
      ////////////////////////////////////////////////////////////////////////////
      when(context.header("X-Forwarded-For")).thenReturn("127.0.0.1");
      when(context.header("X-Real-IP")).thenReturn("127.0.0.1");
      when(context.header("Forwarded")).thenReturn("for=127.0.0.1");
      assertThrows(QAuthenticationException.class, () -> QJavalinImplementation.setupSession(context, null));
      assertThrows(QAuthenticationException.class, () -> ExecutorSessionUtils.setupSession(context, instance));
   }



   /*******************************************************************************
    ** Two simultaneously active request contexts cannot share mutable session state.
    *******************************************************************************/
   @Test
   void testConcurrentRequests() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      String token = UiApiInProcessSessions.register(session(), Duration.ofMinutes(1));
      QContext.init(instance, session());
      CyclicBarrier barrier = new CyclicBarrier(2);
      try(var pool = Executors.newFixedThreadPool(2))
      {
         var first = pool.submit(() -> concurrentSession(instance, token, "one", barrier));
         var second = pool.submit(() -> concurrentSession(instance, token, "two", barrier));
         assertEquals("api/one", first.get(10, TimeUnit.SECONDS));
         assertEquals("api/two", second.get(10, TimeUnit.SECONDS));
      }
      assertEquals(null, UiApiInProcessSessions.resolve(token).orElseThrow().getClientChannel());
      assertEquals(null, UiApiInProcessSessions.resolve(token).orElseThrow().getValue(QSession.VALUE_KEY_USER_TIMEZONE));
   }



   /*******************************************************************************
    ** Hold each session until both requests have applied their own headers.
    *******************************************************************************/
   private String concurrentSession(QInstance instance, String token, String detail, CyclicBarrier barrier) throws Exception
   {
      try
      {
         QSession session = ExecutorSessionUtils.setupSession(context(token, "api/" + detail, detail), instance);
         session.setValue("request", detail);
         barrier.await(5, TimeUnit.SECONDS);
         assertEquals(detail, session.getValue("request"));
         assertEquals(detail, session.getValue(QSession.VALUE_KEY_USER_TIMEZONE));
         return (session.getClientChannel());
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** A registered SSH identity reaches secured specs; bad tokens never use MOCK.
    *******************************************************************************/
   @Test
   void testHttpAuthentication() throws Exception
   {
      server = new QApplicationJavalinServer(new SessionApplication()).withPort(0)
         .withServeFrontendNext(false).withServeFrontendMaterialDashboard(false)
         .withServeLegacyUnversionedMiddlewareAPI(false);
      server.start();
      UiApiInvoker invoker = UiApiInvoker.forServer(server);
      String token = UiApiInProcessSessions.register(session(), Duration.ofMinutes(1));
      UiApiRequest request = new UiApiRequest().withMethod(HttpMethod.GET).withPath("/qbit/test-ext/echo/{word}")
         .withPathParam("word", "session").withCredentials(UiApiCredentials.ofInProcessSession(token));
      UiApiResponse response = invoker.invoke(request);
      assertEquals(200, response.getStatus());
      assertEquals("ssh-user", response.getJson().get("userId").asText());
      assertEquals(200, invoker.invoke(new UiApiRequest().withMethod(HttpMethod.GET).withPath("/metaData")
         .withCredentials(UiApiCredentials.ofInProcessSession(token))).getStatus());
      request.withCredentials(new UiApiCredentials("valid-mock-session", null, null, null, "unknown"));
      assertEquals(401, invoker.invoke(request).getStatus());
      UiApiInProcessSessions.revoke(token);
      request.withCredentials(UiApiCredentials.ofInProcessSession(token));
      assertEquals(401, invoker.invoke(request).getStatus());
   }



   /*******************************************************************************
    ** Application fixture using existing MOCK authentication and echo specs.
    *******************************************************************************/
   public static class SessionApplication extends AbstractQQQApplication
   {
      /*******************************************************************************
       ** Define the test application.
       *******************************************************************************/
      @Override
      public QInstance defineQInstance()
      {
         QInstance instance = TestUtils.defineInstance();
         MiddlewareSpecExtensionsMetaData.ofOrWithNew(instance).withExtension(TestExtensionSpecs.echoExtension());
         return (instance);
      }
   }



   /*******************************************************************************
    ** Test identity representing a user already authenticated with an SSH key.
    *******************************************************************************/
   private static QSession session()
   {
      return (new QSession().withUser(new QUser().withIdReference("ssh-user").withFullName("SSH User"))
         .withPermission("person.READ"));
   }



   /*******************************************************************************
    ** A request context with a real socket peer and per-request headers.
    *******************************************************************************/
   private static Context context(String token, String client, String timezone)
   {
      Context context = mock(Context.class);
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(context.req()).thenReturn(request);
      when(request.getRemoteAddr()).thenReturn("127.0.0.1");
      when(context.header(UiApiInProcessSessions.HEADER)).thenReturn(token);
      when(context.header("X-QQQ-Client")).thenReturn(client);
      when(context.header("X-QQQ-UserTimezone")).thenReturn(timezone);
      return (context);
   }
}
