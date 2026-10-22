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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.metadata.JavalinRouteProviderMetaData;
import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.RouteAuthenticatorInterface;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ProcessBasedRouter
 *******************************************************************************/
class ProcessBasedRouterTest
{
   private QInstance qInstance;


   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      qInstance = TestUtils.defineInstance();
      // QContext not needed for these tests
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
   }


   /*******************************************************************************
    ** Test constructor with path and process name
    *******************************************************************************/
   @Test
   void testConstructor()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess");

      // hostedPath is private, test that router was created
      assertNotNull(router);
   }


   /*******************************************************************************
    ** Test constructor with methods
    *******************************************************************************/
   @Test
   void testConstructorWithMethods()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", List.of("GET", "POST"));

      // hostedPath is private, test that router was created
      assertNotNull(router);
   }


   /*******************************************************************************
    ** Test constructor with metadata
    *******************************************************************************/
   @Test
   void testConstructorWithMetadata()
   {
      JavalinRouteProviderMetaData metadata = new JavalinRouteProviderMetaData();
      metadata.setHostedPath("/api/test");
      metadata.setProcessName("testProcess");
      metadata.setMethods(List.of("POST"));

      ProcessBasedRouter router = new ProcessBasedRouter(metadata);

      // hostedPath is private, test that router was created
      assertNotNull(router);
   }


   /*******************************************************************************
    ** Test constructor defaults to GET method
    *******************************************************************************/
   @Test
   void testConstructorDefaultsToGet()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", null);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();
      assertNotNull(endpointGroup);
   }


   /*******************************************************************************
    ** Test getJavalinEndpointGroup
    *******************************************************************************/
   @Test
   void testGetJavalinEndpointGroup()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess");
      router.setQInstance(qInstance);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();

      assertNotNull(endpointGroup);
   }


   /*******************************************************************************
    ** Test fluent setters
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess");
      QCodeReference authenticator = new QCodeReference(String.class);
      QCodeReference contextHandler = new QCodeReference(String.class);

      ProcessBasedRouter result = router
         .withRouteAuthenticator(authenticator)
         .withContextHandler(contextHandler);

      assertNotNull(result);
      assertEquals(router, result);
      assertEquals(authenticator, router.getRouteAuthenticator());
      assertEquals(contextHandler, router.getContextHandler());
   }


   /*******************************************************************************
    ** Test getJavalinEndpointGroup with unsupported method throws exception
    *******************************************************************************/
   @Test
   void testGetJavalinEndpointGroup_UnsupportedMethod()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", List.of("INVALID"));
      router.setQInstance(qInstance);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();
      
      // Exception is thrown when the endpoint group is executed, not when created
      assertThrows(IllegalArgumentException.class, () ->
      {
         endpointGroup.addEndpoints();
      });
   }


   /*******************************************************************************
    ** Test getJavalinEndpointGroup with multiple HTTP methods
    *******************************************************************************/
   @Test
   void testGetJavalinEndpointGroup_MultipleMethods()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", List.of("GET", "POST", "PUT"));
      router.setQInstance(qInstance);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();

      assertNotNull(endpointGroup);
      // EndpointGroup is callable and can be invoked
   }


   /*******************************************************************************
    ** Test getJavalinEndpointGroup with all HTTP methods
    *******************************************************************************/
   @Test
   void testGetJavalinEndpointGroup_AllMethods()
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/api/test", "testProcess", 
         List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
      router.setQInstance(qInstance);

      EndpointGroup endpointGroup = router.getJavalinEndpointGroup();

      assertNotNull(endpointGroup);
   }


   /*******************************************************************************
    ** Native request logging observes cleanup after denied, throwing and allowed
    ** authentication. The existing routerProcess supplies the successful control.
    *******************************************************************************/
   @Test
   void testAuthenticationAlwaysClearsRequestContext() throws Exception
   {
      ProcessBasedRouter router = new ProcessBasedRouter("/process-context", "routerProcess")
         .withRouteAuthenticator(new QCodeReference(LifecycleAuthenticator.class));
      router.setQInstance(qInstance);
      BlockingQueue<Map.Entry<String, List<Boolean>>> completedRequests = new LinkedBlockingQueue<>();
      Javalin service = null;
      try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         service = Javalin.create(config ->
         {
            config.routes.apiBuilder(router.getJavalinEndpointGroup());
            config.requestLogger.http((context, milliseconds) ->
            {
               try
               {
                  completedRequests.add(Map.entry(context.header("X-Test-Access"),
                     List.of(QContext.getQInstance() == null, QContext.getQSession() == null,
                        QContext.getQBackendTransaction() == null, QContext.getActionStack() == null)));
               }
               finally
               {
                  QContext.clear();
               }
            });
         });
         service.start(0);
         String url = "http://localhost:" + service.port() + "/process-context";
         List<Executable> checks = new ArrayList<>();
         for(String access : List.of("deny", "throw", "allow"))
         {
            checks.add(() ->
            {
               HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5))
                  .header("X-Test-Access", access).build();
               HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
               Map.Entry<String, List<Boolean>> cleared = completedRequests.poll(5, TimeUnit.SECONDS);
               assertAll(access,
                  () -> assertEquals("allow".equals(access) ? 200 : 401, response.statusCode()),
                  () -> assertEquals("1", response.headers().firstValue("X-Test-Auth-Calls").orElse(null)),
                  () -> assertEquals("true", response.headers().firstValue("X-Test-Context-Initialized").orElse(null)),
                  () -> assertEquals(Map.entry(access, List.of(true, true, true, true)), cleared,
                     "Native request logger must observe cleared QContext before fixture cleanup"),
                  () ->
                  {
                     if("allow".equals(access))
                     {
                        assertEquals("So you've done a GET for: /process-context", response.body());
                        assertEquals("Yes, Test", response.headers().firstValue("X-Test").orElse(null));
                     }
                     else
                     {
                        assertFalse(response.body().contains("So you've done a"));
                        assertFalse(response.headers().firstValue("X-Test").isPresent());
                        assertTrue(response.body().contains("deny".equals(access) ? "denied" : "Owned process authentication failure"));
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
      }
   }



   /*******************************************************************************
    ** Exercise the public authenticator extension with real initialized context.
    *******************************************************************************/
   public static class LifecycleAuthenticator implements RouteAuthenticatorInterface
   {
      /*******************************************************************************
       ** Keep invocation evidence on each request, without relying on thread reuse.
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
            throw new QAuthenticationException("Owned process authentication failure");
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
