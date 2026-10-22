/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.RouteAuthenticatorInterface;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Rejecting an authenticator must stop static files, SPA fallback and endpoints.
 *******************************************************************************/
public class IsolatedSpaAuthenticationTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRejectedAuthenticationStopsRequest() throws Exception
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/restricted", "test-spa-admin")
         .withSpaIndexFile("test-spa-admin/index.html").withLoadFromJar(true)
         .withAuthenticator(new QCodeReference(HeaderAuthenticator.class));
      provider.setQInstance(TestUtils.defineInstance());
      Javalin service = Javalin.create(config ->
      {
         provider.acceptJavalinConfig(config);
         config.routes.get("/restricted/endpoint", ctx -> ctx.result("protected endpoint executed"));
      });
      try
      {
         service.start(0);
         try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
         {
            String base = "http://localhost:" + service.port();
            for(String path : new String[] { "/restricted/endpoint", "/restricted", "/restricted/", "/restricted/index.html", "/restricted/deep/link" })
            {
               HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
               assertEquals(401, response.statusCode(), path + ": " + response.body());
               assertFalse(response.body().contains("protected endpoint executed"));
               assertFalse(response.body().contains("<html"));
            }
            HttpResponse<String> allowed = client.send(HttpRequest.newBuilder(URI.create(base + "/restricted/endpoint"))
               .header("X-Test-Access", "allow").timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, allowed.statusCode());
            assertEquals("protected endpoint executed", allowed.body());
            HttpResponse<String> hidden = client.send(HttpRequest.newBuilder(URI.create(base + "/restricted/deep/link"))
               .header("X-Test-Deny-Response", "not-found").timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(404, hidden.statusCode());
            assertFalse(hidden.body().contains("<html"));
         }
      }
      finally
      {
         service.stop();
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Exact and wildcard registrations must not double-run hooks on trailing slash.
    *******************************************************************************/
   @Test
   void testHooksRunOncePerRequest() throws Exception
   {
      IsolatedSpaRouteProvider provider = new IsolatedSpaRouteProvider("/restricted", "test-spa-admin").withLoadFromJar(true)
         .withBeforeHandler(context -> countInvocation(context, "Before"))
         .withAfterHandler(context -> countInvocation(context, "After"))
         .withAuthenticator(new QCodeReference(HeaderAuthenticator.class));
      provider.setQInstance(TestUtils.defineInstance());
      Javalin service = Javalin.create(config ->
      {
         provider.acceptJavalinConfig(config);
         config.routes.get("/restricted", context -> context.result("protected endpoint"));
         config.routes.get("/restricted/child", context -> context.result("protected endpoint"));
         config.routes.get("/restricted-other", context -> context.result("public endpoint"));
      });
      try
      {
         service.start(0);
         try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
         {
            String base = "http://localhost:" + service.port();
            for(String path : new String[] { "/restricted", "/restricted/", "/restricted/child" })
            {
               HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(base + path))
                  .header("X-Test-Access", "allow").timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
               assertEquals(200, response.statusCode(), path);
               assertEquals("protected endpoint", response.body(), path);
               for(String hook : new String[] { "Before", "After", "Authentication" })
               {
                  assertEquals("1", response.headers().firstValue("X-" + hook).orElse("missing"), path + ": " + hook);
               }
            }
            HttpResponse<String> outside = client.send(HttpRequest.newBuilder(URI.create(base + "/restricted-other")).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, outside.statusCode());
            for(String hook : new String[] { "Before", "After", "Authentication" })
            {
               assertFalse(outside.headers().firstValue("X-" + hook).isPresent());
            }
         }
      }
      finally
      {
         service.stop();
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Keep invocation counts on the request so concurrent requests stay isolated.
    *******************************************************************************/
   private static void countInvocation(Context context, String name)
   {
      Integer previous = context.attribute(name);
      Integer count = previous == null ? 1 : previous + 1;
      context.attribute(name, count);
      context.header("X-" + name, count.toString());
   }



   /*******************************************************************************
    ** A status-only denial is a valid authenticator response; false means stop.
    *******************************************************************************/
   public static class HeaderAuthenticator implements RouteAuthenticatorInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public boolean authenticateRequest(Context context)
      {
         countInvocation(context, "Authentication");
         if("allow".equals(context.header("X-Test-Access")))
         {
            return true;
         }
         context.status("not-found".equals(context.header("X-Test-Deny-Response")) ? 404 : 401);
         return false;
      }
   }
}
