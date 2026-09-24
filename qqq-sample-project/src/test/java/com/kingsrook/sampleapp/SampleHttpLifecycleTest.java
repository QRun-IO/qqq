/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.net.BindException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.middleware.javalin.QJavalinMetaData;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import com.kingsrook.qqq.middleware.javalin.routeproviders.IsolatedSpaRouteProvider;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.JavalinConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** First-party server lifecycle contracts through the sample application.
 *******************************************************************************/
class SampleHttpLifecycleTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanUp()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Both route registration APIs run before the retained service observers.
    *******************************************************************************/
   @Test
   void testProviderConfigurationAndServiceObservationOrder() throws Exception
   {
      List<String> order = new ArrayList<>();
      AtomicReference<Javalin> providerService = new AtomicReference<>();
      AtomicReference<Javalin> customizerService = new AtomicReference<>();
      SampleJavalinServer server = newServer();
      server.withAdditionalRouteProviders(List.of(new QJavalinRouteProviderInterface()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public void setQInstance(QInstance instance)
         {
            assertNotNull(instance.getTable("person"));
            order.add("instance");
         }



         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public EndpointGroup getJavalinEndpointGroup()
         {
            order.add("endpoints");
            return () -> ApiBuilder.get("/lifecycle/provider", context -> context.result("provider route"));
         }



         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public void acceptJavalinConfig(JavalinConfig config)
         {
            order.add("provider configuration");
            config.routes.before("/lifecycle/configured", context -> context.attribute("phase", "before"));
         }



         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public void acceptJavalinService(Javalin service)
         {
            order.add("provider service");
            providerService.set(service);
         }
      }));
      server.withJavalinConfigCustomizer(config ->
      {
         order.add("custom configuration");
         config.routes.get("/lifecycle/configured", context -> context.status(202).result(context.attribute("phase") + ":handler"));
         config.routes.after("/lifecycle/configured", context -> context.header("X-Lifecycle", context.result() + ":after"));
      });
      server.withJavalinConfigurationCustomizer(service ->
      {
         order.add("custom service");
         customizerService.set(service);
      });

      try
      {
         server.start();
         assertEquals(List.of("instance", "endpoints", "provider configuration", "custom configuration", "provider service", "custom service"), order);
         assertSame(providerService.get(), customizerService.get());
         try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
         {
            HttpResponse<String> provider = get(client, providerService.get().port(), "/lifecycle/provider");
            assertEquals(200, provider.statusCode());
            assertEquals("provider route", provider.body());
            HttpResponse<String> configured = get(client, providerService.get().port(), "/lifecycle/configured");
            assertEquals(202, configured.statusCode());
            assertEquals("before:handler", configured.body());
            assertEquals("before:handler:after", configured.headers().firstValue("X-Lifecycle").orElseThrow());
         }
      }
      finally
      {
         server.stop();
      }
   }



   /*******************************************************************************
    ** Concurrent servers keep their own fallback and release their listener.
    *******************************************************************************/
   @Test
   void testIndependentSpasAndStopReleasesConfiguredPort() throws Exception
   {
      SampleJavalinServer first = newServer();
      SampleJavalinServer second = newServer();
      SampleJavalinServer replacement = newServer();
      AtomicReference<Javalin> firstService = new AtomicReference<>();
      AtomicReference<Javalin> secondService = new AtomicReference<>();
      AtomicReference<Javalin> replacementService = new AtomicReference<>();
      first.withJavalinConfigurationCustomizer(firstService::set);
      second.withJavalinConfigurationCustomizer(secondService::set);
      replacement.withJavalinConfigurationCustomizer(replacementService::set);
      first.withAdditionalRouteProviders(List.of(new IsolatedSpaRouteProvider("/portal", "site/public").withSpaIndexFile("site/public/index.html").withLoadFromJar(true)));
      second.withAdditionalRouteProviders(List.of(new IsolatedSpaRouteProvider("/portal", "site/private").withSpaIndexFile("site/private/index.html").withLoadFromJar(true)));
      replacement.withJavalinConfigCustomizer(config -> config.routes.get("/replacement", context -> context.result("replacement")));

      try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         first.start();
         second.start();
         for(String path : List.of("/portal/", "/portal/deep/link"))
         {
            HttpResponse<String> publicPage = get(client, firstService.get().port(), path);
            HttpResponse<String> privatePage = get(client, secondService.get().port(), path);
            assertEquals(200, publicPage.statusCode());
            assertEquals(200, privatePage.statusCode());
            assertTrue(publicPage.body().contains("Welcome to the public site"));
            assertFalse(publicPage.body().contains("Welcome to the private site"));
            assertTrue(privatePage.body().contains("Welcome to the private site"));
            assertFalse(privatePage.body().contains("Welcome to the public site"));
         }
         assertEquals(404, get(client, firstService.get().port(), "/portal-other/deep/link").statusCode());
         assertEquals(404, get(client, secondService.get().port(), "/portal/missing.js").statusCode());
         Integer releasedPort = firstService.get().port();
         first.stop();
         replacement.setPort(releasedPort);
         replacement.start();
         assertEquals(releasedPort, replacementService.get().port());
         assertEquals("replacement", get(client, releasedPort, "/replacement").body());
         assertEquals(404, get(client, releasedPort, "/portal/deep/link").statusCode());
         assertTrue(get(client, secondService.get().port(), "/portal/deep/link").body().contains("Welcome to the private site"));
      }
      finally
      {
         replacement.stop();
         second.stop();
         first.stop();
      }
   }



   /*******************************************************************************
    ** A failed bind must leave the existing listener alone and allow later reuse.
    *******************************************************************************/
   @Test
   void testPortConflictRefusesStartupAndCanBeReleased() throws Exception
   {
      SampleJavalinServer occupied = newServer();
      SampleJavalinServer rejected = newServer();
      SampleJavalinServer replacement = newServer();
      AtomicReference<Javalin> occupiedService = new AtomicReference<>();
      AtomicReference<Javalin> service = new AtomicReference<>();
      occupied.withJavalinConfigurationCustomizer(occupiedService::set);
      occupied.withJavalinConfigCustomizer(config -> config.routes.get("/existing", context -> context.result("original listener")));
      replacement.withJavalinConfigurationCustomizer(service::set);
      replacement.withJavalinConfigCustomizer(config -> config.routes.get("/replacement", context -> context.result("ready")));
      try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
      {
         occupied.start();
         Integer port = occupiedService.get().port();
         assertEquals("original listener", get(client, port, "/existing").body());
         rejected.setPort(port);
         RuntimeException failure = assertThrows(RuntimeException.class, rejected::start);
         Throwable cause = failure;
         while(cause.getCause() != null && !(cause instanceof BindException))
         {
            cause = cause.getCause();
         }
         assertTrue(cause instanceof BindException, failure.toString());
         rejected.stop();
         assertEquals("original listener", get(client, port, "/existing").body());
         occupied.stop();
         replacement.setPort(port);
         replacement.start();
         assertEquals(port, service.get().port());
         assertEquals("ready", get(client, port, "/replacement").body());
      }
      finally
      {
         replacement.stop();
         rejected.stop();
         occupied.stop();
      }
   }



   /*******************************************************************************
    ** Use the canonical sample's metadata and database without external auth.
    *******************************************************************************/
   private SampleJavalinServer newServer()
   {
      SampleJavalinServer server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance() throws QException
         {
            return SampleMetaDataProvider.defineTestInstance();
         }
      });
      server.setPort(0);
      server.setServeFrontendMaterialDashboard(false);
      server.setJavalinMetaData(new QJavalinMetaData());
      return server;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> get(HttpClient client, Integer port, String path) throws Exception
   {
      return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
   }
}
