/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.RouteAuthenticatorInterface;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exercise static and fallback routes through a real configured HTTP server.
 *******************************************************************************/
class SimpleFileSystemDirectoryRouterIntegrationTest
{
   /*******************************************************************************
    ** Static content and deep links work; missing assets remain missing.
    *******************************************************************************/
   @Test
   void testSpaDeepLinking() throws Exception
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/site", "public-site")
         .withSpaRootPath("/site").withSpaRootFile("public-site/index.html");
      router.setQInstance(TestUtils.defineInstance());
      String expected;
      try(var resource = getClass().getResourceAsStream("/public-site/index.html"))
      {
         expected = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
      }
      Javalin service = Javalin.create(config ->
      {
         router.acceptJavalinConfig(config);
         config.routes.get("/site/missing-record", context -> context.status(404).result("record missing"));
      });
      try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         service.start(0);
         String base = "http://localhost:" + service.port();
         for(String path : new String[] { "/site/index.html", "/site/deep/link" })
         {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), path);
            assertEquals(expected, response.body(), path);
         }
         for(String path : new String[] { "/site/missing.js", "/outside/deep/link" })
         {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(404, response.statusCode(), path);
         }
         assertAll(
            () ->
            {
               HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + "/site-other/deep/link")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
               assertEquals(404, response.statusCode(), "/site-other/deep/link");
            },
            () ->
            {
               HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + "/site/missing-record")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
               assertEquals(404, response.statusCode(), "/site/missing-record");
               assertEquals("record missing", response.body());
            });
      }
      finally
      {
         service.stop();
      }
   }



   /*******************************************************************************
    ** Authentication covers the root, its trailing slash and actual static/fallback
    ** descendants, with one invocation and an independent public neighbor.
    *******************************************************************************/
   @Test
   void testAuthenticationCoversExactRootAndDescendants() throws Exception
   {
      Boolean originalLoadFromJar = SimpleFileSystemDirectoryRouter.loadStaticFilesFromJar;
      SimpleFileSystemDirectoryRouter.loadStaticFilesFromJar = true;
      Javalin service = null;
      try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         SimpleFileSystemDirectoryRouter router = authenticatedRouter();
         service = Javalin.create(config ->
         {
            router.acceptJavalinConfig(config);
            config.routes.get("/site", context -> context.result("protected root"));
            config.routes.get("/site-other", context -> context.result("public neighbor"));
            config.requestLogger.http((context, milliseconds) -> QContext.clear());
         });
         service.start(0);
         String base = "http://localhost:" + service.port();
         String index = indexContent();
         List<Executable> checks = new ArrayList<>();
         for(String path : List.of("/site", "/site/", "/site/index.html", "/site/deep/link"))
         {
            checks.add(() ->
            {
               HttpResponse<String> denied = request(client, base, path, "deny");
               assertAll(path + " denied",
                  () -> assertEquals(401, denied.statusCode()),
                  () -> assertEquals("denied", denied.body()),
                  () -> assertEquals("1", denied.headers().firstValue("X-Test-Auth-Calls").orElse(null)));
            });
            checks.add(() ->
            {
               HttpResponse<String> allowed = request(client, base, path, "allow");
               assertAll(path + " allowed",
                  () -> assertEquals(200, allowed.statusCode()),
                  () -> assertEquals(path.equals("/site") || path.equals("/site/") ? "protected root" : index, allowed.body()),
                  () -> assertEquals("1", allowed.headers().firstValue("X-Test-Auth-Calls").orElse(null)),
                  () -> assertEquals("true", allowed.headers().firstValue("X-Test-Context-Initialized").orElse(null)));
            });
         }
         checks.add(() ->
         {
            HttpResponse<String> neighbor = request(client, base, "/site-other", "deny");
            assertAll("public neighbor",
               () -> assertEquals(200, neighbor.statusCode()),
               () -> assertEquals("public neighbor", neighbor.body()),
               () -> assertNull(neighbor.headers().firstValue("X-Test-Auth-Calls").orElse(null)));
         });
         assertAll(checks);
      }
      finally
      {
         if(service != null)
         {
            service.stop();
         }
         SimpleFileSystemDirectoryRouter.loadStaticFilesFromJar = originalLoadFromJar;
      }
   }



   /*******************************************************************************
    ** A request logger observes the real servlet thread after terminated auth,
    ** before owned fixture cleanup. Successful static delivery is the control.
    *******************************************************************************/
   @Test
   void testTerminatedAuthenticationClearsRequestContext() throws Exception
   {
      Boolean originalLoadFromJar = SimpleFileSystemDirectoryRouter.loadStaticFilesFromJar;
      SimpleFileSystemDirectoryRouter.loadStaticFilesFromJar = true;
      BlockingQueue<List<Boolean>> completedRequests = new LinkedBlockingQueue<>();
      Javalin service = null;
      try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         SimpleFileSystemDirectoryRouter router = authenticatedRouter();
         service = Javalin.create(config ->
         {
            router.acceptJavalinConfig(config);
            config.requestLogger.http((context, milliseconds) ->
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
            });
         });
         service.start(0);
         String base = "http://localhost:" + service.port();
         String index = indexContent();
         List<Executable> checks = new ArrayList<>();
         for(String access : List.of("deny", "throw", "allow"))
         {
            checks.add(() ->
            {
               HttpResponse<String> response = request(client, base, "/site/index.html", access);
               List<Boolean> cleared = completedRequests.poll(5, TimeUnit.SECONDS);
               assertAll(access,
                  () -> assertEquals("allow".equals(access) ? 200 : 401, response.statusCode()),
                  () -> assertEquals("1", response.headers().firstValue("X-Test-Auth-Calls").orElse(null)),
                  () -> assertEquals("true", response.headers().firstValue("X-Test-Context-Initialized").orElse(null)),
                  () -> assertEquals(List.of(true, true, true, true), cleared, "Native request logger must observe cleared QContext"),
                  () ->
                  {
                     if("allow".equals(access))
                     {
                        assertEquals(index, response.body());
                     }
                     else
                     {
                        assertFalse(response.body().contains("<html"));
                        assertTrue(response.body().contains("deny".equals(access) ? "denied" : "Owned authentication failure"));
                     }
                  });
            });
         }
         assertAll(checks);
      }
      finally
      {
         if(service != null)
         {
            service.stop();
         }
         SimpleFileSystemDirectoryRouter.loadStaticFilesFromJar = originalLoadFromJar;
      }
   }



   /*******************************************************************************
    ** Use existing classpath content and the public authenticator extension.
    *******************************************************************************/
   private SimpleFileSystemDirectoryRouter authenticatedRouter()
   {
      SimpleFileSystemDirectoryRouter router = new SimpleFileSystemDirectoryRouter("/site", "public-site")
         .withSpaRootPath("/site").withSpaRootFile("public-site/index.html")
         .withRouteAuthenticator(new QCodeReference(HeaderAuthenticator.class));
      router.setQInstance(TestUtils.defineInstance());
      return router;
   }



   /*******************************************************************************
    ** Bound actual requests and retain the default no-redirect client behavior.
    *******************************************************************************/
   private HttpResponse<String> request(HttpClient client, String base, String path, String access) throws Exception
   {
      HttpRequest request = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5))
         .header("X-Test-Access", access).build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    ** Independent expected bytes come from the existing fixture resource.
    *******************************************************************************/
   private String indexContent() throws Exception
   {
      try(var resource = getClass().getResourceAsStream("/public-site/index.html"))
      {
         return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
      }
   }



   /*******************************************************************************
    ** Real route authentication has three independently exercised outcomes.
    *******************************************************************************/
   public static class HeaderAuthenticator implements RouteAuthenticatorInterface
   {
      /*******************************************************************************
       ** Count every invocation so overlapping path handlers cannot pass silently.
       *******************************************************************************/
      @Override
      public boolean authenticateRequest(Context context) throws QException
      {
         Integer previousCalls = context.attribute("testAuthCalls");
         Integer calls = previousCalls == null ? 1 : previousCalls + 1;
         context.attribute("testAuthCalls", calls);
         context.header("X-Test-Auth-Calls", calls.toString());
         context.header("X-Test-Context-Initialized", Boolean.toString(QContext.getQInstance() != null && QContext.getQSession() != null));
         String access = context.header("X-Test-Access");
         if("throw".equals(access))
         {
            throw new QAuthenticationException("Owned authentication failure");
         }
         if("allow".equals(access))
         {
            return true;
         }
         context.status(401).result("denied");
         return false;
      }
   }
}
